$(document).ready(function () {

    $.fn.bootstrapSwitch.defaults.size = 'normal';
    $.fn.bootstrapSwitch.defaults.onText = 'Ja';
    $.fn.bootstrapSwitch.defaults.offText = 'Nee';

    $('#friday').bootstrapSwitch();
    $('#saturday').bootstrapSwitch();
    $('#sorting').bootstrapSwitch();
    $('#selling').bootstrapSwitch();
    $('.checkbox label').css('padding-left', 0);


    var not_selling = $('#category option[value="79406b6c-baef-41bd-b646-4d1bf1b1916a"]');
    var selling = $('#selling');
    var category = $('#category_field');

    if (selling.bootstrapSwitch('state'))  enableSelling();
    else disableSelling();

    selling.on('switchChange.bootstrapSwitch', function (event, state) {
        if (state) enableSelling();
        else disableSelling();
    });

    function enableSelling() {
        not_selling.addClass('hidden');
        not_selling.prop('selected', false)
        category.removeClass('hidden')
    }

    function disableSelling() {
        not_selling.removeClass('hidden');
        not_selling.prop('selected', true);
        category.addClass('hidden')
    }

    var selected_organisation = $('#organisation').find(':selected');
    updateSorting(selected_organisation);
    if (selected_organisation.val().length == 0) {
        $('#group option').each(function (i, option) {
            $(option).addClass('hidden')
        });
    } else {
        $('#group option').each(function (i, option) {
            if ($(option).val().length == 0) {
                $(option).text("Selecteer een groep").removeClass('hidden')
            } else if ($(option).val().split('#')[0] == selected_organisation.val()) {
                $(option).removeClass('hidden')
            } else {
                $(option).addClass('hidden')
            }
        })
    }

    $('#organisation').change(function () {
        var organisation = $('#organisation').find(':selected');
        updateGroups(organisation.val());
        updateSorting(organisation)
    });

    function updateGroups(organisation) {
        if (organisation.length == 0) {
            $('#group option').each(function (i, option) {
                if ($(option).val().length == 0) {
                    $(option).text("Selecteer eerst een vereniging").prop('selected', true)
                } else {
                    $(option).addClass('hidden')
                }
            })
        } else {
            $('#group option').each(function (i, option) {
                if ($(option).val().length == 0) {
                    $(option).text("Selecteer een groep").prop('selected', true).removeClass('hidden')
                } else if ($(option).val().split('#')[0] == organisation) {
                    $(option).removeClass('hidden')
                } else {
                    $(option).addClass('hidden')
                }
            })
        }
    }

    function updateSorting(organisation) {
        if (organisation.text() == "Scouting Kapelle" || organisation.val().length == 0) {
            $('#sorting_field').addClass('hidden');
            $('#sorting').prop('checked', false)
        } else {
            $('#sorting_field').removeClass('hidden')
        }
    }
});